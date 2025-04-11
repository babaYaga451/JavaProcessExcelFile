#!/bin/bash

input_file="./src/main/resources/MacyOutput.csv"
output_dir="./target/output"

echo "🔍 Checking for duplicates in output files..."
for file in "$output_dir"/*.txt; do
  duplicates=$(sort "$file" | uniq -d)

  if [[ -n "$duplicates" ]]; then
    echo "🚨 Duplicates found in $file:"
    echo "$duplicates"
  fi
done

echo ""
echo "📊 Verifying line counts..."

input_count=$(wc -l < "$input_file")
output_count=$(cat "$output_dir"/*.txt | wc -l)

expected_output_count=$((input_count - 1)) # subtracting 1 for header

echo "Input line count        : $input_count"
echo "Expected output count   : $expected_output_count"
echo "Actual output line count: $output_count"

if [[ "$output_count" -eq "$expected_output_count" ]]; then
  echo "✅ All lines accounted for."
else
  echo "❌ Line count mismatch. Some lines may be missing!"
fi
