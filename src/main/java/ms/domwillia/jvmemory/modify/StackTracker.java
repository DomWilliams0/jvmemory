package ms.domwillia.jvmemory.modify;

import java.util.Stack;

public class StackTracker {
	private static Stack<Frame> stack = new Stack<>();

	public static void push(String clazz, String method) {
		stack.push(new Frame(clazz, method));
		System.out.printf(":::::::: PUSH %s:%s : %s\n", prettyClass(clazz), method, stack);
	}

	public static void pop() {
		stack.pop();
		System.out.printf(":::::::: POP : %s\n", stack);
	}

	private static String prettyClass(String clazz) {
		return clazz.substring(clazz.lastIndexOf('/') + 1);
	}

	static class Frame {
		final String clazz;
		final String method;

		public Frame(String clazz, String method) {
			this.clazz = clazz;
			this.method = method;
		}

		@Override
		public String toString() {
			return String.format("%s:%s", prettyClass(clazz), method);
		}
	}
}
