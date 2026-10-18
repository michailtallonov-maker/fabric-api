# Выдаёт декоративную рамку портала Энда (с уже вставленным Оком).
# Сама команда /function доступна только операторам (уровень прав 2+),
# дополнительно проверяем, что игрок в креативе.
execute unless entity @s[type=player] run return fail
execute unless entity @s[gamemode=creative] run tellraw @s {text:"Эта команда работает только в креативе.",color:"red"}
execute unless entity @s[gamemode=creative] run return fail

give @s minecraft:end_portal_frame[minecraft:block_state={eye:"true"},minecraft:custom_name={text:"Рамка портала (декор)",color:"light_purple",italic:false},minecraft:lore=[{text:"Только для красоты — портал не откроет",color:"gray",italic:false}],minecraft:enchantment_glint_override=true] 12
tellraw @s {text:"Выдано 12 декоративных рамок портала.",color:"green"}
