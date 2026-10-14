package com.bottlecast.app.common.components.bottle

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.PathParser

/**
 * The bottle rig's shapes, from TEMPLATE.md's rig, in its 140 × 220 viewBox. The glass is the only
 * part that changes with the silhouette; the scroll, cork and highlights sit in the same places.
 */
internal object BottleRigPaths {
    const val WIDTH = 140f
    const val HEIGHT = 220f

    /** Where the neck opens: the scroll is behind the glass below this, and in front above it. */
    const val NECK_MOUTH_Y = 34f

    val classic: Path by lazy { parse("M60 34H80V64C80 76 112 82 112 106V190C112 204 102 212 88 212H52C38 212 28 204 28 190V106C28 82 60 76 60 64Z") }
    val flask: Path by lazy { parse("M60 34H80V70H106C111 70 114 73 114 78V196C114 205 108 212 99 212H41C32 212 26 205 26 196V78C26 73 29 70 34 70H60Z") }
    val gourd: Path by lazy { parse("M60 34H80V58C96 62 100 76 92 90C116 100 118 130 114 160C110 198 92 212 70 212C48 212 30 198 26 160C22 130 24 100 48 90C40 76 44 62 60 58Z") }

    // The long highlight down the glass; the flask's wider shoulders move it out to x≈36.
    val bodyHighlight: Path by lazy { parse("M40 112C37 140 37 168 41 194") }
    val flaskHighlight: Path by lazy { parse("M36 112C33 140 33 168 37 194") }
    val neckHighlight: Path by lazy { parse("M66 40V60") }

    val cork: Path by lazy { parse("M60 8H80L78 32H62Z") }
    val corkLines: Path by lazy { parse("M63 15H77M63 22H77") }

    fun body(silhouette: BottleSilhouette): Path = when (silhouette) {
        BottleSilhouette.Classic -> classic
        BottleSilhouette.Flask -> flask
        BottleSilhouette.Gourd -> gourd
    }

    fun highlight(silhouette: BottleSilhouette): Path =
        if (silhouette == BottleSilhouette.Flask) flaskHighlight else bodyHighlight

    private fun parse(pathData: String): Path = PathParser().parsePathString(pathData).toPath()
}
