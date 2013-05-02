#!/bin/bash
#

MESSAGE="$1"

ROOM='Mercury%20%26%20Athena%20(GPInfx)'
TOKEN='ec3ec0a0fe096691c10e771e66a437'
USER='Erik%20Husby'
echo "$MESSAGE" | /prodinfo/prodapps/releng/bin/hipchat_room_message -t $TOKEN -r "$ROOM" -f "$USER"
