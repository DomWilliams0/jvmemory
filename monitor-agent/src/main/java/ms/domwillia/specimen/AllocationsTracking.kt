package ms.domwillia.specimen

class AllocationsTracking : Specimen {

    override fun go() {
        var last = Link(0)
        for (i in 1..5) {
            val l = Link(i)
            last.next = l
            last = l
        }

        var strLast = Link("first")
        for (i in 1..5) {
            val l = Link(i.toString())
            strLast.next = l
            strLast = l
        }

        //		LinkHolder holder = new LinkHolder();
        //		holder.a = last;
        //		holder.b = strLast;
    }

    private inner class LinkHolder {
        internal var a: Link<*>? = null
        internal var b: Link<*>? = null
    }

    private inner class Link<T> internal constructor(internal var value: T) {
        internal var next: Link<*>? = null
    }
}
