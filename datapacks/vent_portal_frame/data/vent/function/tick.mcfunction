# ПКМ предметом «Портал Энда» (только в креативе)
execute as @a[scores={vent.use=1..},gamemode=creative] if items entity @s weapon.mainhand minecraft:carrot_on_a_stick[minecraft:custom_data~{vent_portal:1b}] at @s anchored eyes positioned ^ ^ ^ run function vent:use
scoreboard players reset @a[scores={vent.use=1..}] vent.use
