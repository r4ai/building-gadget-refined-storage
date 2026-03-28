param(
    [string]$WorldDatapacksDir = "run/saves/New World/datapacks",
    [string]$PackName = "rs_item_seed",
    [int]$MaxItems = 5000,
    [int]$BatchSize = 9
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.IO.Compression.FileSystem

$root = Split-Path -Parent $PSScriptRoot
$packDir = Join-Path $root $WorldDatapacksDir | Join-Path -ChildPath $PackName
$functionsDir = Join-Path $packDir "data/seed5000/function"
$batchDir = Join-Path $functionsDir "batches"
$waitDir = Join-Path $functionsDir "wait"
$tagDir = Join-Path $packDir "data/minecraft/tags/function"
$itemListPath = Join-Path $packDir "item_ids.txt"

$jarPaths = @(
    "C:\Users\r4ai\.gradle\caches\neoformruntime\artifacts\minecraft_1.21.1_client.jar",
    "C:\Users\r4ai\.gradle\caches\modules-2\files-2.1\com.refinedmods.refinedstorage\refinedstorage-neoforge\2.0.1\9096e0109a55ab066675bd453663d114bdefc99a\refinedstorage-neoforge-2.0.1.jar",
    "C:\Users\r4ai\.gradle\caches\modules-2\files-2.1\curse.maven\mekanism-268560\7621971\2c48df86350bb733e4285437e136b271a70f4cbc\mekanism-268560-7621971.jar",
    "C:\Users\r4ai\.gradle\caches\modules-2\files-2.1\curse.maven\building-gadgets-2-298187\6850515\666a155981e3cd231f67809b9e2a0238377bbbfc\building-gadgets-2-298187-6850515.jar",
    (Join-Path $root "build/libs/buildinggadgetrefinedstorage-0.1.0-SNAPSHOT.jar")
) | Where-Object { Test-Path $_ }

if (-not $jarPaths) {
    throw "No jars found to scan for item IDs."
}

function Test-ProbablyRealItemId {
    param([string]$NamespaceId)

    $path = $NamespaceId.Split(":", 2)[1]
    $leaf = Split-Path $path -Leaf

    if ($leaf -in @(
            "generated",
            "handheld",
            "shield_blocking",
            "trident_in_hand",
            "spyglass_in_hand"
        )) {
        return $false
    }

    if ($leaf.StartsWith("template_")) {
        return $false
    }

    return $true
}

function Get-ItemIdsFromJar {
    param([string]$JarPath)

    $zip = [System.IO.Compression.ZipFile]::OpenRead($JarPath)
    try {
        $translatedIds = New-Object System.Collections.Generic.HashSet[string]
        foreach ($entry in $zip.Entries) {
            if ($entry.FullName -notmatch '^assets/([^/]+)/lang/.+\.json$') {
                continue
            }

            $namespace = $matches[1]
            $reader = New-Object System.IO.StreamReader($entry.Open())
            try {
                $json = $reader.ReadToEnd() | ConvertFrom-Json -AsHashtable
            } finally {
                $reader.Dispose()
            }

            foreach ($key in $json.Keys) {
                if ($key -match "^(?:item|block)\.$([Regex]::Escape($namespace))\.(.+)$") {
                    [void]$translatedIds.Add("${namespace}:$($matches[1])")
                }
            }
        }

        $result = New-Object System.Collections.Generic.HashSet[string]
        foreach ($entry in $zip.Entries) {
            $fullName = $entry.FullName

            if ($fullName -match '^assets/([^/]+)/models/item/(.+)\.json$') {
                $id = "$($matches[1]):$($matches[2] -replace '\\','/')"
                if ((Test-ProbablyRealItemId $id) -and $translatedIds.Contains($id)) {
                    [void]$result.Add($id)
                }
            }
        }

        return $result
    } finally {
        $zip.Dispose()
    }
}

function New-FunctionText {
    param([string[]]$Lines)
    return ($Lines -join "`n") + "`n"
}

$allIds = New-Object System.Collections.Generic.HashSet[string]
foreach ($jarPath in $jarPaths) {
    foreach ($id in Get-ItemIdsFromJar $jarPath) {
        [void]$allIds.Add($id)
    }
}

$selectedIds = $allIds |
    Sort-Object |
    Select-Object -First $MaxItems

if (-not $selectedIds) {
    throw "No item IDs found."
}

if (Test-Path $packDir) {
    Remove-Item -Recurse -Force $packDir
}

New-Item -ItemType Directory -Force -Path $batchDir | Out-Null
New-Item -ItemType Directory -Force -Path $waitDir | Out-Null
New-Item -ItemType Directory -Force -Path $tagDir | Out-Null

$itemListContent = @(
    "# Generated from jar assets on $(Get-Date -Format s)"
    "# Total found: $($allIds.Count)"
    "# Selected: $($selectedIds.Count)"
    $selectedIds
)
Set-Content -Path $itemListPath -Value $itemListContent -Encoding UTF8

$packMcmeta = @'
{
  "pack": {
    "pack_format": 48,
    "description": "Seeds a chest with many item types for RS importer testing"
  }
}
'@
Set-Content -Path (Join-Path $packDir "pack.mcmeta") -Value $packMcmeta -Encoding UTF8

$loadTag = @'
{
  "values": [
    "seed5000:load"
  ]
}
'@
Set-Content -Path (Join-Path $tagDir "load.json") -Value $loadTag -Encoding UTF8

$loadFunction = New-FunctionText @(
    "data modify storage seed5000:state active set value 0b",
    "tellraw @a [{""text"":""[seed5000] /function seed5000:start で開始, /function seed5000:stop で停止"",""color"":""yellow""}]"
)
Set-Content -Path (Join-Path $functionsDir "load.mcfunction") -Value $loadFunction -Encoding UTF8

$startFunction = New-FunctionText @(
    "data modify storage seed5000:state active set value 1b",
    "tellraw @a [{""text"":""[seed5000] start: chest 4 -60 -4 に順次投入を開始"",""color"":""green""}]",
    "schedule function seed5000:wait/0000 1t replace"
)
Set-Content -Path (Join-Path $functionsDir "start.mcfunction") -Value $startFunction -Encoding UTF8

$stopFunction = New-FunctionText @(
    "data modify storage seed5000:state active set value 0b",
    "tellraw @a [{""text"":""[seed5000] stop"",""color"":""red""}]"
)
Set-Content -Path (Join-Path $functionsDir "stop.mcfunction") -Value $stopFunction -Encoding UTF8

$batches = @()
for ($offset = 0; $offset -lt $selectedIds.Count; $offset += $BatchSize) {
    $batches += ,@($selectedIds[$offset..([Math]::Min($offset + $BatchSize - 1, $selectedIds.Count - 1))])
}

$finishFunction = New-FunctionText @(
    "data modify storage seed5000:state active set value 0b",
    "tellraw @a [{""text"":""[seed5000] 完了: $($selectedIds.Count) 種類を投入対象にしました"",""color"":""aqua""}]"
)
Set-Content -Path (Join-Path $functionsDir "finish.mcfunction") -Value $finishFunction -Encoding UTF8

for ($i = 0; $i -lt $batches.Count; $i++) {
    $batch = $batches[$i]
    $lines = New-Object System.Collections.Generic.List[string]
    $lines.Add("# Batch $i")
    for ($slot = 0; $slot -lt $batch.Count; $slot++) {
        $lines.Add("item replace block 4 -60 -4 container.$slot with $($batch[$slot]) 1")
    }
    if ($i -lt ($batches.Count - 1)) {
        $lines.Add("schedule function seed5000:wait/{0:d4} 1t replace" -f ($i + 1))
    } else {
        $lines.Add("schedule function seed5000:finish 1t replace")
    }
    $path = Join-Path $batchDir ("{0:d4}.mcfunction" -f $i)
    Set-Content -Path $path -Value (New-FunctionText $lines) -Encoding UTF8
}

for ($i = 0; $i -lt $batches.Count; $i++) {
    $waitLines = @(
        "# Wait for chest to empty before next batch",
        ("execute if data storage seed5000:state {{active:1b}} unless data block 4 -60 -4 Items[] run function seed5000:batches/{0:d4}" -f $i),
        ("execute if data storage seed5000:state {{active:1b}} if data block 4 -60 -4 Items[] run schedule function seed5000:wait/{0:d4} 1t replace" -f $i)
    )
    $path = Join-Path $waitDir ("{0:d4}.mcfunction" -f $i)
    Set-Content -Path $path -Value (New-FunctionText $waitLines) -Encoding UTF8
}

$readme = @(
    "Chest target: 4 -60 -4",
    "Item list source: item_ids.txt",
    "Start with: /function seed5000:start",
    "Stop with: /function seed5000:stop",
    "Behavior: when the chest is empty, the next batch of $BatchSize unique items is inserted."
)
Set-Content -Path (Join-Path $packDir "README.txt") -Value $readme -Encoding UTF8

Write-Host "Generated datapack at $packDir"
Write-Host "Found $($allIds.Count) item IDs, selected $($selectedIds.Count), created $($batches.Count) batch functions."
