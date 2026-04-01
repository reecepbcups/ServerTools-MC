jar_name := "servertools-7.1.0.jar"
output_dir := "../output"
plugins_dir := "server/plugins"

build:
    mvn package -q -DskipTests
    mkdir -p {{plugins_dir}}
    cp {{output_dir}}/{{jar_name}} {{plugins_dir}}/{{jar_name}}
    @echo "deployed {{jar_name}} -> {{plugins_dir}}"

# start paper server (first run downloads paper + generates world)
server-up:
    docker compose up

server-down:
    docker compose down

# send a command to the mc console (e.g. just mc-cmd "reload confirm")
mc-cmd cmd:
    docker compose exec mc rcon-cli {{cmd}}

# --- e2e tests ---

# run e2e tests against running server
test-e2e:
    node --test loadtest/e2e/*.test.mjs

# run a single e2e test file (e.g. just test-e2e-file commands)
test-e2e-file name:
    node --test loadtest/e2e/{{name}}.test.mjs

# --- load testing ---

# install loadtest deps (one-time)
loadtest-setup:
    cd loadtest && npm install

# run load test (e.g. just loadtest 20 move 3)
loadtest bots="10" scenario="all" ramp="3":
    node loadtest/loadtest.mjs --bots {{bots}} --scenario {{scenario}} --ramp {{ramp}}

# run load test for a fixed duration in seconds
loadtest-timed bots="10" scenario="all" duration="60":
    node loadtest/loadtest.mjs --bots {{bots}} --scenario {{scenario}} --ramp 3 --duration {{duration}}

# ramp bots until TPS drops below 18 (finds your breaking point)
loadtest-ramp bots="100":
    node loadtest/loadtest.mjs --bots {{bots}} --scenario all --ramp-test true

# start spark profiler for N seconds
loadtest-profile duration="60":
    just mc-cmd "spark profiler start --timeout {{duration}}"

# stop spark profiler and get results
loadtest-results:
    just mc-cmd "spark profiler stop"

# check current TPS via spark
loadtest-tps:
    just mc-cmd "spark tps"

# full cycle: build plugin, start spark, run bots, collect results
loadtest-full bots="50" duration="120":
    just build
    just mc-cmd "spark profiler start --timeout {{duration}}"
    node loadtest/loadtest.mjs --bots {{bots}} --scenario all --ramp 3 --duration {{duration}}
    just mc-cmd "spark profiler stop"
