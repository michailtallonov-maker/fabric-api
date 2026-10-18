package io.github.michailtallonov.goodfooding;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Zeroes the equip-progress ("hand height") fields of the first-person hands, which lowers them below the screen
 * the same way vanilla does while switching items. Looked up reflectively so a renamed field only disables this
 * cosmetic tweak instead of crashing the game.
 */
public final class HandHider {
	private static final String[] FIELD_NAMES = {"mainHandHeight", "oMainHandHeight", "offHandHeight", "oOffHandHeight"};

	private static @Nullable List<Field> fields;

	private HandHider() {
	}

	public static void lower(Object hands) {
		for (Field field : fields(hands.getClass())) {
			try {
				field.setFloat(hands, 0.0F);
			} catch (IllegalAccessException ignored) {
				// setAccessible succeeded during lookup, so this can't happen
			}
		}
	}

	private static List<Field> fields(Class<?> type) {
		if (fields == null) {
			List<Field> found = new ArrayList<>();

			for (String name : FIELD_NAMES) {
				try {
					Field field = type.getDeclaredField(name);

					if (field.getType() == float.class) {
						field.setAccessible(true);
						found.add(field);
					}
				} catch (NoSuchFieldException | RuntimeException ignored) {
					// field doesn't exist in this version
				}
			}

			fields = found;
		}

		return fields;
	}
}
