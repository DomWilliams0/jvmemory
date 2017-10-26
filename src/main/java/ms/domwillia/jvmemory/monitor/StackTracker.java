package ms.domwillia.jvmemory.monitor;

import java.util.Stack;

@SuppressWarnings("unused")
public class StackTracker {
	private static Stack<Frame> stack = new Stack<>();

	public static void push(String clazz, String method) {
		stack.push(new Frame(clazz, method));
		System.out.println(">>> " + getHead());
	}

	public static void pop() {
		System.out.println("<<< " + getHead());
		stack.pop();
	}

	private static String prettyClass(String clazz) {
		return clazz.substring(clazz.lastIndexOf('/') + 1);
	}

	public static String getHead() {
		return stack.peek().toString();
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
