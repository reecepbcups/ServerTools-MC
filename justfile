jar_name := "servertools-7.1.0.jar"
output_dir := "../output"
plugins_dir := "server/plugins"

build:
    mvn compile

package:
    mvn package -q -DskipTests

# build jar and copy to local server plugins
deploy: package
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

# full cycle: build, deploy, hot-reload plugin via PlugManX
e2e: deploy
    docker compose exec mc rcon-cli "plugman reload ServerTools"
    @echo "plugin reloaded"
