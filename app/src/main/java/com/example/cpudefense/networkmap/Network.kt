package com.example.cpudefense.networkmap

import android.graphics.*
import android.view.MotionEvent
import com.example.cpudefense.EndlessStageCreator
import com.example.cpudefense.GameMechanics
import com.example.cpudefense.GameView
import com.example.cpudefense.gameElements.Chip
import com.example.cpudefense.gameElements.GameElement
import com.example.cpudefense.gameElements.Vehicle
import java.util.concurrent.CopyOnWriteArrayList

class Network(val gameMechanics: GameMechanics, val gameView: GameView, x: Int, y: Int): GameElement() {
    data class Data(
        var gridSizeX: Int = 1,
        var gridSizeY: Int = 1,
        var nodes: HashMap<Int, Node.Data> = hashMapOf(),
        var links: HashMap<Int, Link.Data> = hashMapOf(),
        var tracks: HashMap<Int, Track.Data> = hashMapOf(),
        var vehicles: List<Vehicle.Data> = listOf(),
        var sectorSizeX: Int = 0,  // only relevant for debugging purposes, if the network is divided in sectors
        var sectorSizeY: Int = 0,
    )

    var data = Data(gridSizeX = x, gridSizeY = y)

    var nodes = hashMapOf<Int, Node>()
    var links = hashMapOf<Int, Link>()
    var vehicles: CopyOnWriteArrayList<Vehicle> = CopyOnWriteArrayList<Vehicle>()

    var backgroundImage: Bitmap? = null
    private lateinit var networkImage: Bitmap
    private var paint = Paint()
    private var gridPointDistance: Pair<Int, Int>? = null

    enum class Dir { HORIZONTAL, VERTICAL, DIAGONAL, REVERSE_DIAGONAL, UNDEFINED }

    companion object {
        const val minVehicleSpeed = GameMechanics.minAttackerSpeed
    }

    fun distanceBetweenGridPoints(): Pair<Int, Int>?
            /**
             * Calculate the distance on the screen between two grid points. (This is a property of the
             * network under a given viewport and does not depend on the actual network elements.)
             *
             * @return 2-dimensional distance vector (x,y), or null if the distance cannot be determined.
             */
    {
        if (gridPointDistance != null)
            return gridPointDistance // no need to recalculate
        if (validateViewport()) {
            val point0 = gameView.viewport.gridToViewport(Coord(0, 0))
            val point1 = gameView.viewport.gridToViewport(Coord(1, 1))
            gridPointDistance = Pair(point1.first - point0.first, point1.second - point0.second)
        }
        else
            gridPointDistance = null // screen size not known, can't determine distance
        return gridPointDistance
    }

    override fun update() {
        for (obj in nodes.values)
            obj.update()
        for (obj in links.values)
            obj.update()
        for (obj in vehicles)
            obj.update()
    }

    fun validateViewport(): Boolean
    /** The viewport needs the size of the game surface (GameView) to calculate positions on the screen.
     * However, it is not easy to know when GameView actually receives its dimensions.
     * Therefore we keep track whether the scaling factors are valid, and if not, we recalculate them.
     *
     * @return false if the viewport is not valid, i.e. screen dimensions are not known
     */
    {
        if (gameView.width > 0 || gameView.height > 0) // TODO: ersetzen durch Test auf Initialisierung
        {
            gameView.viewport.setScreenSize(gameView.width, gameView.viewportHeight(gameView.height))
            return true
        }
        else
            return false
    }

    override fun display(canvas: Canvas, viewport: Viewport)
    {
        // if viewport is not valid, try to validate it
        if (!viewport.isValid && !validateViewport())
            return
        displayNetwork(canvas, viewport)
        for (obj in nodes.values)
            obj.display(canvas, viewport)
        for (obj in vehicles)
            obj.display(canvas, viewport)
        for (obj in nodes.values)
            (obj as Chip).displayUpgrades(canvas)
        if (false)  // for debugging purposes
            if (data.sectorSizeX > 0 && data.sectorSizeY > 0)
                EndlessStageCreator.displaySectors(canvas, viewport, data)
    }

    fun makeSnapshot(canvas: Canvas, viewport: Viewport)
    {
        for (obj in links.values)
            obj.display(canvas, viewport)
        for (obj in nodes.values) {
            obj.display(canvas, viewport)
        }
    }

    private fun displayNetwork(canvas: Canvas, viewport: Viewport)
    /** draws all 'fixed' elements, i.e. the background image and the links */
    {
        // displayFrame(canvas, viewport) // optional
        if (!this::networkImage.isInitialized)
            recreateNetworkImage(viewport)
        canvas.drawBitmap(this.networkImage, null, viewport.screen, paint)
    }

    fun recreateNetworkImage(viewport: Viewport)
    /** function that must be called whenever the viewport (or the network configuration) changes.
     * It either takes the provided background image or an empty background
     * and places the network elements on it */
    {
        validateViewport()
        gameView.background.createImage(gameMechanics.currentStage)
        gameView.background.basicBackground?.let {
            val canvas = Canvas(it)
            for (obj in links.values)
                obj.display(canvas, viewport)
        }
    }

    fun addNode(node: Node?, ident: Int = -1): Int
            /**
             * @param ident If no ident is given, a new one is created.
             * @return Ident of the node
             */
    {
        if (node == null)
            return -1
        val nodeId = if (ident >= 0) ident else nodes.size + 1
        node.let { nodes[nodeId] = it }
        return nodeId
    }

    fun addLink(link: Link?, ident: Int = -1): Int
            /**
             * @param ident If no ident is given, a new one is created.
             * @return Ident of the link
             */
    {
        val linkId = if (ident >= 0) ident else links.size + 1
        link?.let { links[linkId] = it }
        return linkId
    }

    fun createTrack(ident: Int, linkIdents: List<Int>, isCircle: Boolean): Track
    {
        val track = Track(this)
        track.data.ident = ident
        track.data.linkIdents = linkIdents
        track.data.isCircle = isCircle
        linkIdents.forEach {id ->
            links[id]?.let {
                track.links.add(it)
                it.usageCount += 1
            }
        }
        return track
    }

    fun addVehicle(vehicle: Vehicle)
    /** adds an existing vehicle to the network */
    {
        vehicles.add(vehicle)
    }

    fun onLongPress(p0: MotionEvent?) {
    }
}