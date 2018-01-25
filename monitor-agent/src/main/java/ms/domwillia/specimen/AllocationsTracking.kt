package ms.domwillia.specimen

class AllocationsTracking : Specimen {

    lateinit var a: IntLink
    lateinit var b: StrLink

    override fun go() {
        var last = IntLink(0, null)
        for (i in 1..5) {
            val l = IntLink(i, null)
            last.next = l
            last = l
        }

        var strLast = StrLink("first", null)
        for (i in 1..5) {
            val l = StrLink(i.toString(), null)
            strLast.next = l
            strLast = l
        }

        a = last
        b = strLast

        val getFieldTest = a
    }

    data class IntLink(val value: Int, var next: IntLink?)
    data class StrLink(val value: String, var next: StrLink?)
}
