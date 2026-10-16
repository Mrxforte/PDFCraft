package com.example

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class SmallpdfApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            PDFBoxResourceLoader.init(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
