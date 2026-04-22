Add-Type -AssemblyName System.Drawing
function Is-Foreground($c) { if ($c.A -lt 16) { return $false }; return ($c.R + $c.G + $c.B) -gt 36 }
function Get-ImagePixels($bmp) { $pixels = New-Object 'System.Drawing.Color[,]' $bmp.Width, $bmp.Height; for ($y=0; $y -lt $bmp.Height; $y++) { for ($x=0; $x -lt $bmp.Width; $x++) { $pixels[$x,$y] = $bmp.GetPixel($x,$y) } }; return $pixels }
function Get-BoundingBox($pixels, $width, $height) { $minX=$width; $minY=$height; $maxX=-1; $maxY=-1; for ($y=0; $y -lt $height; $y++) { for ($x=0; $x -lt $width; $x++) { if (Is-Foreground $pixels[$x,$y]) { if ($x -lt $minX) { $minX=$x }; if ($y -lt $minY) { $minY=$y }; if ($x -gt $maxX) { $maxX=$x }; if ($y -gt $maxY) { $maxY=$y } } } }; if ($maxX -lt 0) { return $null }; return @{ MinX=$minX; MinY=$minY; MaxX=$maxX; MaxY=$maxY } }
function Get-ConnectedComponents($pixels, $width, $height) {
  $visited = New-Object 'bool[,]' $width, $height; $components=@(); $dirs=@(@(1,0),@(-1,0),@(0,1),@(0,-1),@(1,1),@(1,-1),@(-1,1),@(-1,-1))
  for ($y=0; $y -lt $height; $y++) { for ($x=0; $x -lt $width; $x++) { if ($visited[$x,$y] -or -not (Is-Foreground $pixels[$x,$y])) { continue }
    $queue=[System.Collections.Generic.Queue[object]]::new(); $queue.Enqueue(@($x,$y)); $visited[$x,$y]=$true; $minX=$x; $maxX=$x; $minY=$y; $maxY=$y; $count=0
    while ($queue.Count -gt 0) { $point=$queue.Dequeue(); $px=[int]$point[0]; $py=[int]$point[1]; $count++; if ($px -lt $minX) { $minX=$px }; if ($px -gt $maxX) { $maxX=$px }; if ($py -lt $minY) { $minY=$py }; if ($py -gt $maxY) { $maxY=$py }
      foreach ($d in $dirs) { $nx=$px+[int]$d[0]; $ny=$py+[int]$d[1]; if ($nx -lt 0 -or $ny -lt 0 -or $nx -ge $width -or $ny -ge $height) { continue }; if ($visited[$nx,$ny] -or -not (Is-Foreground $pixels[$nx,$ny])) { continue }; $visited[$nx,$ny]=$true; $queue.Enqueue(@($nx,$ny)) }
    }
    if ($count -ge 40) { $components += @{ MinX=$minX; MinY=$minY; MaxX=$maxX; MaxY=$maxY; Count=$count } }
  } }
  return $components | Sort-Object MinX
}
function Crop-ToBitmap($source,$box,$padding) { $x=[Math]::Max(0,$box.MinX-$padding); $y=[Math]::Max(0,$box.MinY-$padding); $w=[Math]::Min($source.Width-$x,($box.MaxX-$box.MinX+1)+($padding*2)); $h=[Math]::Min($source.Height-$y,($box.MaxY-$box.MinY+1)+($padding*2)); $target=New-Object System.Drawing.Bitmap $w,$h; $g=[System.Drawing.Graphics]::FromImage($target); $g.DrawImage($source,0,0,([System.Drawing.Rectangle]::new($x,$y,$w,$h)),[System.Drawing.GraphicsUnit]::Pixel); $g.Dispose(); return $target }
function Make-Transparent($bmp) { $target=New-Object System.Drawing.Bitmap $bmp.Width,$bmp.Height; $transparent=[System.Drawing.Color]::FromArgb(0,0,0,0); for ($y=0; $y -lt $bmp.Height; $y++) { for ($x=0; $x -lt $bmp.Width; $x++) { $c=$bmp.GetPixel($x,$y); if (Is-Foreground $c) { $target.SetPixel($x,$y,$c) } else { $target.SetPixel($x,$y,$transparent) } } }; return $target }
function Resize-ToItem($source,$destPath) {
  $transparent=[System.Drawing.Color]::FromArgb(0,0,0,0); $dest=New-Object System.Drawing.Bitmap 16,16; for ($y=0; $y -lt 16; $y++) { for ($x=0; $x -lt 16; $x++) { $dest.SetPixel($x,$y,$transparent) } }
  $pixels=Get-ImagePixels $source; $box=Get-BoundingBox $pixels $source.Width $source.Height; if ($null -eq $box) { throw "No visible pixels found for $destPath" }
  $cropped=Crop-ToBitmap $source $box 0; $scale=[Math]::Min(14.0 / $cropped.Width, 14.0 / $cropped.Height); $newW=[Math]::Max(1,[int][Math]::Round($cropped.Width * $scale)); $newH=[Math]::Max(1,[int][Math]::Round($cropped.Height * $scale)); $offsetX=[int][Math]::Floor((16 - $newW)/2); $offsetY=[int][Math]::Floor((16 - $newH)/2)
  $g=[System.Drawing.Graphics]::FromImage($dest); $g.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor; $g.PixelOffsetMode=[System.Drawing.Drawing2D.PixelOffsetMode]::Half; $g.CompositingMode=[System.Drawing.Drawing2D.CompositingMode]::SourceCopy; $g.Clear($transparent); $g.DrawImage($cropped,[System.Drawing.Rectangle]::new($offsetX,$offsetY,$newW,$newH),0,0,$cropped.Width,$cropped.Height,[System.Drawing.GraphicsUnit]::Pixel); $g.Dispose(); $dest.Save($destPath,[System.Drawing.Imaging.ImageFormat]::Png); $cropped.Dispose(); $dest.Dispose()
}
$pngs = Get-ChildItem 'C:\Users\tg\Downloads' -Filter *.png | ForEach-Object {
  $img = [System.Drawing.Image]::FromFile($_.FullName)
  [PSCustomObject]@{ FullName = $_.FullName; Width = $img.Width; Height = $img.Height }
}
$toolsFile = $pngs | Where-Object { $_.Width -gt $_.Height } | Select-Object -First 1
$copperFile = $pngs | Where-Object { $_.Width -eq $_.Height } | Select-Object -First 1
if ($null -eq $toolsFile -or $null -eq $copperFile) { throw 'Source images not identified by dimensions.' }
$itemDir = 'G:\myfix\src\main\resources\assets\harder_beginnings\textures\item'
$tools = [System.Drawing.Bitmap]::FromFile($toolsFile.FullName)
$components = Get-ConnectedComponents (Get-ImagePixels $tools) $tools.Width $tools.Height
if ($components.Count -ne 3) { throw "Expected 3 tool components, found $($components.Count)" }
$toolTargets = @('flint_axe.png','flint_shovel.png','flint_knife.png')
for ($i=0; $i -lt 3; $i++) { $crop = Crop-ToBitmap $tools $components[$i] 4; $transparentCrop = Make-Transparent $crop; Resize-ToItem $transparentCrop (Join-Path $itemDir $toolTargets[$i]); $transparentCrop.Dispose(); $crop.Dispose() }
$tools.Dispose()
$copper = [System.Drawing.Bitmap]::FromFile($copperFile.FullName)
$copperTransparent = Make-Transparent $copper
Resize-ToItem $copperTransparent (Join-Path $itemDir 'copper_dust.png')
$copperTransparent.Dispose(); $copper.Dispose()
$previewFiles = @((Join-Path $itemDir 'flint_axe.png'),(Join-Path $itemDir 'flint_shovel.png'),(Join-Path $itemDir 'flint_knife.png'),(Join-Path $itemDir 'copper_dust.png'))
$sheet = New-Object System.Drawing.Bitmap 128,32; $g=[System.Drawing.Graphics]::FromImage($sheet); $g.InterpolationMode=[System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor; $g.PixelOffsetMode=[System.Drawing.Drawing2D.PixelOffsetMode]::Half; $g.Clear([System.Drawing.Color]::FromArgb(255,24,20,18)); for ($i=0; $i -lt $previewFiles.Count; $i++) { $img=[System.Drawing.Image]::FromFile($previewFiles[$i]); $g.DrawImage($img,$i*32,0,32,32); $img.Dispose() }; $previewPath='G:\myfix\tmp_extracted_item_preview.png'; $sheet.Save($previewPath,[System.Drawing.Imaging.ImageFormat]::Png); $g.Dispose(); $sheet.Dispose(); Write-Output $previewPath
