$content = Get-Content FE/dashboard.html -Raw -Encoding UTF8

# Remove CSS for Chatbot
$content = $content -replace '(?s)/\* Chatbot UI \*/.*?</style>', '</style>'

# Remove HTML for Chatbot
$content = $content -replace '(?s)<!-- Chatbot Widget -->.*?(?=<script>)', ''

# Remove Chatbot JS logic
$content = $content -replace '(?s)// ========================== CHATBOT LOGIC ==========================.*?function openChangePasswordModal', 'function openChangePasswordModal'

[IO.File]::WriteAllText((Resolve-Path FE/dashboard.html).Path, $content, [System.Text.Encoding]::UTF8)
Write-Output "Cleaned up dashboard.html"
