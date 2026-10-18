# Выдаёт предмет, который ставит блок «Портал Энда».
# /function доступна только операторам (уровень прав 2+), плюс нужен креатив.
execute unless entity @s[type=player] run return fail
execute unless entity @s[gamemode=creative] run tellraw @s {text:"Эта команда работает только в креативе.",color:"red"}
execute unless entity @s[gamemode=creative] run return fail

give @s minecraft:carrot_on_a_stick[minecraft:custom_data={vent_portal:1b},minecraft:item_model="minecraft:ender_eye",minecraft:custom_name={text:"Портал Энда",color:"light_purple",italic:false},minecraft:lore=[{text:"ПКМ по полу — поставить портал (декор)",color:"gray",italic:false},{text:"Shift + ПКМ по нему — убрать",color:"gray",italic:false}],minecraft:enchantment_glint_override=true,minecraft:unbreakable={}]
tellraw @s {text:"Выдан «Портал Энда». ПКМ по полу — поставить, Shift + ПКМ — убрать.",color:"green"}
