#!/bin/bash

# author: Xi Wang

# terminate the instance


while IFS='' read -r line || [[ -n "$line" ]]; do
    echo "Text read from file: $line"
    aws ec2 terminate-instances --instance-ids $line
done < "id.txt"

#aws ec2 terminate-instances --instance-ids $instance_id