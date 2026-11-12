/**
 * Pro:Centric proxy setup for Vortex consoles with LG 750E tuners.
 *
 * ## Overview
 * Pro:Centric is LG's hotel/hospitality TV platform. On Vortex consoles, the console acts as the
 * Pro:Centric (update) server by running [Caddy](https://caddyserver.com) as a local reverse HTTP proxy.
 *
 * ## Request flow
 * 1. The LG 750E tuner sends a DNS query to resolve `procentric.local`.
 * 2. dnsmasq (configured via [DNSMASQ_CONF]) resolves `procentric.local` to [VORTEX_INTERNAL_IP] —
 *    the console's own IP on the internal network.
 * 3. The tuner fetches config files via Caddy, which reverse-proxies those requests from
 *    [VORTEX_INTERNAL_IP]:80 upstream to [PROCENTRIC_UPSTREAM_HOST]:443.
 * 4. The tuner uses the URLs within those config files to fetch middleware and firmware payloads
 *    directly from [PROCENTRIC_UPSTREAM_HOST], bypassing the console.
 *
 * This proxy is only needed for initial tuner setup (or after a manual reset). Once the tuner is
 * initialized, the middleware reconfigures its Pro:Centric server address to point directly at
 * [PROCENTRIC_UPSTREAM_HOST], after which the console is no longer in the request path.
 *
 * Caddy is only launched if its binary is present on the device — consoles without the required
 * OS update are unaffected.
 *
 * ## References
 * - US9 Pro:Centric Proxy Setup: https://jhtnaee.atlassian.net/wiki/spaces/TT/pages/6710820888/US9+-+ProCentric+Proxy+Server
 */
package com.jht.chimera.service.hal

import android.util.Log
import com.jht.chimera.lib.common.ExecuteAsRoot
import com.jht.chimera.lib.common.config.ProductConfig
import java.io.File

private const val TAG = "ProcentricSetup"

private val VORTEX_INTERNAL_IP = ProductConfig.VORTEX_INTERNAL_IP

private const val CONFIGS_DIR = "/sdcard/jht/configs"
private const val CADDY_JSON = "$CONFIGS_DIR/caddy.json"
private const val DNSMASQ_CONF = "$CONFIGS_DIR/dnsmasq.conf"
private const val PROCENTRIC_UPSTREAM_HOST = "lg-prod.aws-jhtna.net"

// Common install locations for the caddy binary; checked in order.
private val CADDY_BIN_CANDIDATES = listOf("/system/bin/caddy", "/vendor/bin/caddy")

private val DNSMASQ_CONTENT = """
    # Vortex config that maps internal network procentric.local to $VORTEX_INTERNAL_IP
    listen-address=$VORTEX_INTERNAL_IP
    address=/procentric.local/$VORTEX_INTERNAL_IP
    interface=eth1
""".trimIndent()

private val CADDY_JSON_CONTENT = """
{
	"logging": {
		"logs": {
			"default": {
				"writer": {"output": "file", "filename": "$CONFIGS_DIR/caddy-access.log"},
				"level": "DEBUG"
			}
		}
	},
	"apps": {
		"http": {
			"servers": {
				"srv0": {
					"listen": [
						"$VORTEX_INTERNAL_IP:80"
					],
					"logs": {},
					"routes": [
						{
							"handle": [
								{
									"handler": "reverse_proxy",
									"headers": {
										"request": {
											"set": {
												"Host": [
													"$PROCENTRIC_UPSTREAM_HOST"
												]
											}
										}
									},
									"transport": {
										"protocol": "http",
										"tls": {}
									},
									"upstreams": [
										{
											"dial": "$PROCENTRIC_UPSTREAM_HOST:443"
										}
									]
								}
							]
						}
					]
				}
			}
		}
	}
}
""".trimIndent() + "\n"

/**
 * Launches caddy as a reverse proxy for Pro:Centric on Vortex consoles.
 * No-ops if the caddy binary isn't found (devices without the OS update are unaffected).
 */
fun startProcentricProxy() {
    if (!isCaddyPresent()) {
        Log.i(TAG, "startProcentricProxy: caddy binary not found, skipping")
        return
    }
    Log.i(TAG, "startProcentricProxy: upstream=$PROCENTRIC_UPSTREAM_HOST ip=$VORTEX_INTERNAL_IP")
    writeConfigFiles()
    launchCaddy()
}

private fun isCaddyPresent() = CADDY_BIN_CANDIDATES.any { File(it).exists() }

private fun writeConfigFiles() {
    Log.d(TAG, "writeConfigFiles: ensuring $CONFIGS_DIR exists")
    try {
        val dir = File(CONFIGS_DIR)
        val dirCreated = dir.mkdirs()
        Log.d(TAG, "writeConfigFiles: configs dir exists=${dir.exists()} created=$dirCreated")

        val dnsmasqFile = File(DNSMASQ_CONF)
        if (!dnsmasqFile.exists() || dnsmasqFile.readText() != DNSMASQ_CONTENT) {
            dnsmasqFile.writeText(DNSMASQ_CONTENT)
            Log.i(TAG, "writeConfigFiles: wrote $DNSMASQ_CONF")
        } else {
            Log.d(TAG, "writeConfigFiles: $DNSMASQ_CONF already up to date")
        }

        val caddyJsonFile = File(CADDY_JSON)
        if (!caddyJsonFile.exists() || caddyJsonFile.readText() != CADDY_JSON_CONTENT) {
            caddyJsonFile.writeText(CADDY_JSON_CONTENT)
            Log.i(TAG, "writeConfigFiles: wrote $CADDY_JSON (${CADDY_JSON_CONTENT.length} bytes)")
        } else {
            Log.d(TAG, "writeConfigFiles: $CADDY_JSON already up to date")
        }
    } catch (e: Exception) {
        Log.e(TAG, "writeConfigFiles: failed to write procentric config files", e)
    }
}

private fun launchCaddy() {
    Log.i(TAG, "launchCaddy: launching caddy proxy")
    val output = StringBuilder()
    // nohup keeps caddy alive after the su shell exits. sleep 1 lets port 80 clear before rebind.
    val success = ExecuteAsRoot()
        .addCommand("killall caddy 2>/dev/null; true")
        .addCommand("sleep 1")
        .addCommand("nohup caddy run --config $CADDY_JSON >> $CONFIGS_DIR/caddy.log 2>&1 &")
        .execute(false /* synchronous */, true, output)
    if (output.isNotEmpty()) Log.d(TAG, "launchCaddy: output -> $output")
    if (success) {
        Log.i(TAG, "launchCaddy: caddy procentric proxy launched successfully")
    } else {
        Log.e(TAG, "launchCaddy: ExecuteAsRoot returned failure, caddy may not have started")
    }
}
