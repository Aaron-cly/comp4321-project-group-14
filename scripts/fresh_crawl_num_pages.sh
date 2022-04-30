cd ..

gradle -x test

# Change the number after FRESH_CRAWL to the target number of pages to be crawled
gradle run --args="FRESH_CRAWL $1"

cd scripts