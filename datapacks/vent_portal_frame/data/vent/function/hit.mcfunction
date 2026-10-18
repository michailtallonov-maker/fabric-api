# Shift + ПКМ по декоративному порталу — убрать его
execute if entity @s[predicate=vent:sneaking] if block ~ ~ ~ minecraft:barrier if block ~ ~-1 ~ minecraft:end_portal run return run function vent:remove
execute if entity @s[predicate=vent:sneaking] run return fail

# ПКМ по блоку пола: этот блок становится невидимым барьером,
# а под ним ставится блок портала Энда. По барьеру можно ходить,
# портал виден сквозь него, но до него не дотронуться — телепорта нет.
setblock ~ ~ ~ minecraft:barrier
setblock ~ ~-1 ~ minecraft:end_portal
playsound minecraft:block.end_portal_frame.fill block @s ~ ~ ~
