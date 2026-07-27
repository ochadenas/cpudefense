package com.example.cpudefense.networkmap

import com.example.cpudefense.Stage
import java.util.concurrent.CopyOnWriteArrayList

class Track(val network: Network) {
    data class Data(
        var ident: Int,
        var linkIdents: List<Int>,/** list of idents */
        var isCircle: Boolean
    )

    var data = Data(
        ident = 0,
        linkIdents = listOf(),
        isCircle = false
    )

    var links: CopyOnWriteArrayList<Link> = CopyOnWriteArrayList()

    fun nextLink(link: Link): Link?
    {
        if (links.isEmpty())
            return null
        val index = links.indexOfFirst { it == link }
        if (index == links.lastIndex)
            return if (data.isCircle) links[0] else null
        else
            return links[index+1]
    }

    /** create a deep copy of the track. Optionally, use a new ident given as parameter */
    fun copy(newIdent: Int? = null): Track
    {
        val target = Track(network)
        target.data = data.copy()
        target.links = CopyOnWriteArrayList(links)
        newIdent?.let { data.ident = newIdent }
        return target
    }


    companion object {
        fun createFromData(stage: Stage, data: Data): Track
        {
            val track = Track(stage.network)
            track.data = data
            for (id in data.linkIdents)
                track.links.add(stage.network.links[id])
            return track
        }
    }

}