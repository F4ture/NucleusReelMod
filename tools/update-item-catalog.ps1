param([Parameter(Mandatory=$true)][string]$ItemsJson)
$items = (Get-Content -LiteralPath $ItemsJson -Raw | ConvertFrom-Json).items
$ids = @('DIVAN_ALLOY','PET_ITEM_QUICK_CLAW','DIVAN_FRAGMENT','JADERALD','GEMSTONE_MIXTURE','HELIX','CLAW_FOSSIL','PRECIOUS_PEARL','PREHISTORIC_EGG','BOB_OMB','RECALL_POTION','OIL_BARREL','PICKONIMBUS','GOBLIN_EGG','GOBLIN_EGG_RED','GOBLIN_EGG_BLUE','GOBLIN_EGG_GREEN','GOBLIN_EGG_YELLOW')
foreach ($gem in @('RUBY','AMBER','TOPAZ','JADE','SAPPHIRE','AMETHYST','JASPER')) {
    foreach ($quality in @('ROUGH','FLAWED','FINE','FLAWLESS','PERFECT')) { $ids += "${quality}_${gem}_GEM" }
    $ids += "${gem}_CRYSTAL"
}
$catalog = @()
foreach ($id in $ids) {
    $item = $items | Where-Object id -eq $id | Select-Object -First 1
    if ($null -eq $item) { throw "Missing API item: $id" }
    $material = switch ($item.material) { 'SKULL_ITEM' { 'player_head' } 'INK_SACK' { 'purple_dye' } default { $item.material.ToLowerInvariant() } }
    $catalog += [ordered]@{ id=$id; name=$item.name; material=$material; itemModel=$item.item_model; tier=$item.tier; glowing=($item.glowing -eq $true); skinValue=$item.skin.value; skinSignature=$item.skin.signature }
}
foreach ($book in @(@('lapidary',1,'Lapidary I'),@('fortune',4,'Fortune IV'))) {
    $catalog += [ordered]@{ id='ENCHANTED_BOOK'; name="Enchanted Book ($($book[2]))"; material='enchanted_book'; tier='COMMON'; glowing=$true; enchantment=$book[0]; enchantmentLevel=$book[1] }
}
$catalog += [ordered]@{ id='ENCHANTED_BOOK'; name='Enchanted Book'; material='enchanted_book'; tier='COMMON'; glowing=$true }
$destination = Join-Path $PSScriptRoot '../src/main/resources/assets/nucleusreel/rewards.json'
New-Item -ItemType Directory -Force (Split-Path -Parent $destination) | Out-Null
[System.IO.File]::WriteAllText([System.IO.Path]::GetFullPath($destination), ($catalog | ConvertTo-Json -Depth 5), [System.Text.UTF8Encoding]::new($false))
Write-Output "Generated $($catalog.Count) reward definitions."
