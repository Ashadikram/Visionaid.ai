package com.college.visionaid_ai

import android.content.Context
import android.content.Intent
import com.google.ai.client.generativeai.type.content

class AppOpenHandler(private val context: Context) {

    fun openCalculator(): String {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage("com.miui.calculator")

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                "Opining calculator"
            } else {
                "calculator app not found"
            }
        } catch (e: Exception) {
            "Error opening calculator ${e.message}"
        }
            /*
                        val pm = context.packageManager
                        val intent = Intent(Intent.ACTION_MAIN, null)

                        intent.addCategory(Intent.CATEGORY_LAUNCHER)

                        val apps = pm.queryIntentActivities(intent, 0)

                        for (app in apps) {
                            val appName = app.loadLabel(pm).toString().lowercase()

                            if (appName.contains("Calculator") || appName.contains("calc") || appName.contains("mi calculator")) {
                                val packageName = app.activityInfo.packageName
                                val launchIntent = pm.getLaunchIntentForPackage(packageName)

                                if(launchIntent != null) {
                                    launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(launchIntent)
                                    return "Opening calculator"
                                }
                            }
                        }
                        "Calculator app not found"
                    } catch (e: Exception){
                        "Error opening calculator ${e.message}"
                    }*/
        }
            /*
            val  intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_APP_CALCULATOR)

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                "Opening calculator"
            } else {
                //fallbac: open any calculator app manually

                val fallbackIntent = context.packageManager.getLaunchIntentForPackage("com.android.calculator2")
                if (fallbackIntent != null){
                    context.startActivity(fallbackIntent)
                    "Opening calculator"
                } else {
                    "No Calculator app found"
                }
            }

        } catch (e: Exception) {
            "Calculator app not found on this device"
        }*/
    }
