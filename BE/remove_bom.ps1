$files = Get-ChildItem -Path "src\main\java\com\example\auth\entity" -Filter "*.java"
foreach ($file in $files) {
    $content = [System.IO.File]::ReadAllText($file.FullName)
    [System.IO.File]::WriteAllText($file.FullName, $content, (New-Object System.Text.UTF8Encoding($false)))
}
