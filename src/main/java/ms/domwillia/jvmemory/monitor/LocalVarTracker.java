package ms.domwillia.jvmemory.monitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalVarTracker {
	// class:method -> {index, name}
	private static Map<String, List<LocalVar>> vars = new HashMap<>();

	public static void registerLocalVar(String clazz, String method, String name, int index) {
		String key = formatClassAndMethod(clazz, method);
		List<LocalVar> locals = vars.getOrDefault(key, new ArrayList<>());
		locals.add(new LocalVar(index, name));
		vars.put(key, locals);
	}

	private static String formatClassAndMethod(String clazz, String method) {
		return String.format("%s:%s", clazz, method);
	}

	public static void clear() {
		vars.clear();
	}

	public static void debugPrint() {
		for (Map.Entry<String, List<LocalVar>> entry : vars.entrySet()) {
			System.out.printf("%s:\n", entry.getKey());
			for (LocalVar var : entry.getValue()) {
				System.out.printf("\t%d - %s\n", var.index, var.name);
			}
		}
	}

	private static class LocalVar {
		int index;
		String name;
		// TODO type? or derive from usage

		public LocalVar(int index, String name) {
			this.index = index;
			this.name = name;
		}
	}
}
