$lines = Get-Content FE/dashboard.html -Encoding UTF8

# Remove HTML (lines 597-680 in original file -> indices 596-679)
$part1 = $lines[0..595]
$part2 = $lines[680..($lines.Length-1)]
$lines = $part1 + $part2

# Remove CSS (lines 326-542 in original file -> indices 325-541)
$part1 = $lines[0..324]
$part2 = $lines[542..($lines.Length-1)]
$lines = $part1 + $part2

[IO.File]::WriteAllLines((Resolve-Path FE/dashboard.html).Path, $lines, [System.Text.Encoding]::UTF8)
Write-Output "Removed Chatbot HTML & CSS"
