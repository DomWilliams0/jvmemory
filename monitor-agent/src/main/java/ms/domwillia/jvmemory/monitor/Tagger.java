package ms.domwillia.jvmemory.monitor;

public class Tagger {

	public static String internalName = Tagger.class.getTypeName();

	/**
	 * Generates a new tag for a new object
	 */
	public static native long allocateTag(Object o);

	/**
	 * Assigns the last allocated tag to this object, without allocating a new one
	 * Handy for super classes
	 */
	@SuppressWarnings("unused")
	public static native void assignCurrentTag(Object o);

	/**
	 * Gets the tag of the current
	 */
	public static native long getTag(Object o);
}
