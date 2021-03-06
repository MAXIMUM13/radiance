/*
 * Copyright (c) 2005-2020 Radiance Kirill Grouchnikov. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  o Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  o Neither the name of the copyright holder nor the names of
 *    its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.pushingpixels.lucent

import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import org.pushingpixels.lucent.content.AlbumOverviewPanel
import org.pushingpixels.lucent.data.SearchResultRelease
import org.pushingpixels.lucent.details.DetailsWindowManager
import org.pushingpixels.meteor.addDelayedMouseListener
import org.pushingpixels.meteor.addDelayedWindowFocusListener
import java.awt.*
import javax.swing.JFrame

class MainWindow : JFrame("Lumen demo") {
    private val contentPanel: AlbumOverviewPanel
    private val closeButton: CloseButton

    init {
        this.isUndecorated = true
        this.background = Color(0, 0, 0, 0)
        this.getRootPane().putClientProperty("Window.shadow", java.lang.Boolean.FALSE)

        this.contentPanel = AlbumOverviewPanel()
        this.closeButton = CloseButton()

        val contentPane = this.contentPane
        contentPane.add(this.contentPanel)
        contentPane.add(this.closeButton)

        contentPane.setComponentZOrder(this.contentPanel, 1)
        contentPane.setComponentZOrder(this.closeButton, 0)

        contentPane.layout = object : LayoutManager {
            override fun addLayoutComponent(name: String, comp: Component) {}

            override fun removeLayoutComponent(comp: Component) {}

            override fun minimumLayoutSize(parent: Container): Dimension? {
                return null
            }

            override fun preferredLayoutSize(parent: Container): Dimension? {
                return null
            }

            override fun layoutContainer(parent: Container) {
                val closeButtonDim = 35
                closeButton.setBounds(width - closeButtonDim, 0,
                        closeButtonDim, closeButtonDim)
                contentPanel.setBounds(0, 10, width - 10, height - 10)
            }
        }

        this.addDelayedWindowFocusListener(
                onWindowLostFocus = { DetailsWindowManager.disposeCurrentlyShowing() })

        this.setSize(560, 250)
        this.setLocationRelativeTo(null)
    }

    fun addAlbumItem(album: SearchResultRelease): AlbumOverviewComponent {
        return contentPanel.addAlbumItem(album)
    }

    fun setLoading(isLoading: Boolean) {
        contentPanel.setLoading(isLoading)
    }

    fun setSearchString(searchString: String) {
        contentPanel.setSearchString(searchString)
    }

    fun doLoad(searchStringDisplayable: String, searchString: String) {
        GlobalScope.launch(Dispatchers.Swing) {
            setLoading(true)
            setSearchString(searchStringDisplayable)

            // Load the data off the EDT / UI thread
            val data = GlobalScope.async(Dispatchers.IO) {
                val result = BackendConnector.doAlbumSearch(searchString, searchStringDisplayable)
                // simulate longer load if required
                if (System.getProperty("lucent.load") != null) {
                    delay(2000)
                }
                System.err.println("Done searching")
                result
            }

            // await() will suspend until the data is loaded, freeing EDT in the meanwhile.
            // Once the data is loaded, it will come back here to populate the list of albums
            for (album in data.await()) {
                val albumOverviewComp = addAlbumItem(album)
                albumOverviewComp.addDelayedMouseListener(
                        onMouseClicked = { DetailsWindowManager.show(this@MainWindow, album) })
            }

            invalidate()
            validate()
            repaint()

            setLoading(false)
        }
    }
}
