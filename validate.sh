#!/bin/bash

# Get list of present shippers from filenames (without extension)
ls ./target/output/automation | sed 's/\..*//' | sort | uniq > present_shippers.txt

# Print lines from data.csv where the shipper is not present in the output
echo "🚨 Missing shipper lines in data.csv:"
while IFS=',' read -r col1 col2; do
    if ! grep -Fxq "$col2" present_shippers.txt; then
        echo "$col1,$col2"
    fi
done < src/main/resources/origin_shipper.csv