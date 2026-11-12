package com.jht.updated.bundler

import getGitInfo
import getLocalVersion
import org.gradle.api.Project
import java.util.Locale

/** Responsible for creating the manifest files. */
object ManifestMaker {
    private const val ROOT_ELEMENT = "root"

    /**
     * Create the manifest file using the given information.
     *
     * @param ota       whether its over the air, defaults to false
     * @param isNightly defaults to false
     */
    fun createManifest(project: Project, flavor: JHTFlavor, bundles: List<Bundle>, ota: Boolean = false, isNightly: Boolean = false): String {
        val groupedBundles = getBundleGroups(bundles)

        val versionNumbers = if (isNightly) {
            getLocalVersion(getGitInfo(project.rootDir)).split(".").toTypedArray()
        } else {
            flavor.bundleVersion.split(".").toTypedArray()
        }

        val major = versionNumbers[0]
        val minor = versionNumbers[1]
        val patch = versionNumbers[2]
        val build = versionNumbers[3]
        val manifest =  "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                        "<$ROOT_ELEMENT equipment=\"${flavor.equipment}\" requires_reboot=\"true\" major=\"$major\" minor=\"$minor\" patch=\"$patch\" build=\"$build\">\n" +
                        "</$ROOT_ELEMENT>"
        return addBundleGroupsToManifest(manifest, groupedBundles, project, flavor, ota, isNightly)
    }

    fun createManifest(project: Project, flavor: JHTFlavor, bundle: Bundle, ota: Boolean = false): String =
        createManifest(project, flavor, listOf(bundle), ota)

    fun getBundleGroups(bundles: List<Bundle>) : Map<String,List<Bundle>> {
        val ret: MutableMap<String, MutableList<Bundle>> = mutableMapOf();
        bundles.forEach{
            if(!ret.containsKey(it.getPackageGroup())) {
                ret.put(it.getPackageGroup(), mutableListOf());
            }
            ret.get(it.getPackageGroup())?.add(it);
        }
        return ret;
    }

    fun addBundleGroupsToManifest(manifest: String, groupedBundles: Map<String, List<Bundle>>, project: Project, flavor: JHTFlavor, ota: Boolean = false, isNightly: Boolean = false) : String {
        val lines = manifest.trimEnd().split('\n') as MutableList
        var order = 1
        //Find current highest order to place new bundles after all existing packages
        for (line in lines) {
            val lineOrder = line.substringAfter("order=\"").substringBefore('\"').toIntOrNull()
            if (lineOrder != null && lineOrder > order) {
                order = lineOrder
            }
        }

        //Need to make sure CORE Packages are listed before all other groups
        if(groupedBundles.containsKey(CORE_PACKAGE_GROUP)) {
            val versionNumbers = if (isNightly) {
                getLocalVersion(getGitInfo(project.rootDir)).split(".").toTypedArray()
            } else {
                flavor.bundleVersion.split(".").toTypedArray()
            }

            val major = versionNumbers[0]
            val minor = versionNumbers[1]
            val patch = versionNumbers[2]
            val build = versionNumbers[3]

            // Add the version info to the CORE bundle grouping.
            lines.add(lines.size - 1,"<$CORE_PACKAGE_GROUP major=\"$major\" minor=\"$minor\" patch=\"$patch\" build=\"$build\">")
            groupedBundles[CORE_PACKAGE_GROUP]?.forEach {
                lines.add(
                    lines.size - 1,
                    it.getManifestXML(project, flavor, order++, ota, isNightly)
                )
            }
            lines.add(lines.size - 1, "</$CORE_PACKAGE_GROUP>")
        }


        for (bundleGroup in groupedBundles) {
            println("Adding Bundle Group: ${bundleGroup.key} with length: ${bundleGroup.value.size}")
            //Because we already did CORE PACKAGE ABOVE don't double it up
            if(bundleGroup.key != CORE_PACKAGE_GROUP) {
                lines.add(lines.size - 1,"<${bundleGroup.key}>")

                    bundleGroup.value.forEach {
                        try {
                            lines.add(
                                lines.size - 1,
                                it.getManifestXML(project, flavor, order++, ota, isNightly)
                            )
                        } catch (e: Exception) {
                            println("Exception while adding package: " + it.packageShortName);
                            e.printStackTrace();
                        }
                    }

                lines.add(lines.size - 1, "</${bundleGroup.key}>")
            }
        }

        return StringBuilder().apply {
            for (line in lines) {
                appendLine(line)
            }
        }.toString().trimEnd()
    }

    fun removeTPAPackagesFromManifest(manifest: String) : String {
        // Makes this more reliable to get rid of any potential new lines at the end
        val cleanedManifest = manifest.trimEnd()
        val lines = cleanedManifest.split('\n') as MutableList
        val linesIterator = lines.iterator();
        while(linesIterator.hasNext()) {
            val name = linesIterator.next().substringAfter("name=\"")
                .substringBefore('\"').lowercase(Locale.ROOT)
            if (name.contains("third") && name.contains("party")
                && name.contains("app")) {
                linesIterator.remove()
            }
        }

        return StringBuilder().apply {
            for (line in lines) {
                appendLine(line)
            }
        }.toString()
    }
}