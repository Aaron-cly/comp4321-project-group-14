# run the script in root directory bash scripts/run_crawler.sh FRESH_CRAWL

./gradlew build -x test

if [ $1 == "FRESH_CRAWL" ]; then
  if [ "$#" -ne 2 ]; then
    ./gradlew run --args="FRESH_CRAWL"
  else
    ./gradlew run --args="FRESH_CRAWL $2"
  fi
fi

if [ $1 != "FRESH_CRAWL" ]; then
  if [ "$#" -ne 2 ]; then
    ./gradlew run --args="EXISTING_CRAWL"
  else
    ./gradlew run --args="EXISTING_CRAWL $2"
  fi
fi
