#!/bin/bash

emrinput=0

sanityCheck () {
	: "${MR_INPUT:?Please set the input data path in MR_INPUT!}"
}

clean () {
	rm -rf output target
	rm -rf log
	sbt clean clean-cache clean-lib clean-plugins
}

cmp () {
	# building the code
	sbt assembly
}

upload_data_emr() { # upload the data files
	aws s3 rm s3://${BUCKET_NAME}/input --recursive
	aws s3 sync ${MR_INPUT} s3://${BUCKET_NAME}/input
}

local_mode () {
	rm -rf output
	sbt "run ${MR_INPUT} output local"
}

emr () {
	# set aws credentials
	export AWS_DEFAULT_REGION=us-west-2

	# clean the files
	aws s3 rm s3://${BUCKET_NAME}/output --recursive
	aws s3 rm s3://${BUCKET_NAME}/Job.jar

	# upload jar file
	aws s3 cp target/scala-2.10/Job.jar s3://${BUCKET_NAME}/Job.jar

	# configure EMR
	loguri=s3n://${BUCKET_NAME}/log/

	cid=$(aws emr create-cluster --applications Name=Hadoop Name=Spark\
		--ec2-attributes '{"InstanceProfile":"EMR_EC2_DefaultRole"}' \
		--service-role EMR_DefaultRole \
		--enable-debugging \
		--release-label emr-4.3.0 \
		--log-uri ${loguri} \
		--steps '[{"Args":["spark-submit","--deploy-mode","cluster","--class","analysis.MissedConnections","s3://'${BUCKET_NAME}'/Job.jar","s3://'${BUCKET_NAME}'/input","s3://'${BUCKET_NAME}'/output"],"Type":"CUSTOM_JAR","ActionOnFailure":"CONTINUE","Jar":"command-runner.jar","Properties":"","Name":"MissedConnectionsAnalysis"}]' \
		--name 'Spark cluster' \
		--instance-groups '[{"InstanceCount":1,"InstanceGroupType":"MASTER","InstanceType":"m1.medium","Name":"Master Instance Group"},{"InstanceCount":4,"InstanceGroupType":"CORE","InstanceType":"m1.medium","Name":"Core Instance Group"}]' \
		--configurations '[{"Classification":"spark","Properties":{"maximizeResourceAllocation":"true"},"Configurations":[]}]' \
		--auto-terminate \
		--region us-west-2 | grep -oh 'j-[0-9A-Z][0-9A-Z]*')

	# waiting for job to complete
	tmstate='            "State": "TERMINATED",'
	output=$(aws emr describe-cluster --output json --cluster-id "$cid" | grep -oh '^[[:space:]]\{12\}"State": "TERMINATED",')
	while [ "$output" != "$tmstate" ]; do
		output=$(aws emr describe-cluster --cluster-id "$cid" | grep -oh '^[[:space:]]\{12\}"State": "TERMINATED",')
		echo Waiting for job completion...
		sleep 1m
	done

	# get the output
	aws s3 sync s3://${BUCKET_NAME}/output output --delete
}

get_output_emr () {
	aws s3 sync s3://${BUCKET_NAME}/output output --delete
}

if [ $1 = '-cmp' ]; then
	cmp
fi

input_path='input'

if [ "$1" = '-local' ]; then
	# clean
	local_mode
	# process_output
fi

if [ "$1" = '-emr' ]; then
	emr
fi

if [ "$1" = '-full-emr' ]; then
	clean
	cmp
	upload_data_emr
	emr
fi

