execute unless block ~ ~ ~ #minecraft:air run return run function vent:hit
scoreboard players remove @s vent.ray 1
execute if score @s vent.ray matches 1.. positioned ^ ^ ^0.1 run function vent:ray
