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
