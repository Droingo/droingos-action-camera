$ErrorActionPreference = "Stop"

$ProjectRoot = (Get-Location).Path

if (!(Test-Path ".\gradlew.bat")) {
    throw "Run this from the root of droingos-action-camera."
}

$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$BackupRoot = Join-Path $ProjectRoot "_operator_control_backup_$Timestamp"
$TempRoot = Join-Path $env:TEMP "droingo_operator_control_$Timestamp"
$PayloadZip = Join-Path $TempRoot "payload.zip"
$ExtractRoot = Join-Path $TempRoot "payload"

New-Item -ItemType Directory -Force -Path $BackupRoot | Out-Null
New-Item -ItemType Directory -Force -Path $ExtractRoot | Out-Null

$PayloadBase64 = @'
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
YS9uZXQvZHJvaW5nby9hY3Rpb25jYW1lcmEvY2xpZW50L0NsaWVudEdhbWVFdmVudHMuamF2Ye08a3fbuLHf/SsQ9dxeee3Q3m3rpnaSVtEj1lnZ8rEUp9me
Hh+YhCRuKFIlSNvau/7vnQH4AEiQouRke7ctP9gUMBgA88LMYKQVtT/TOSM+iywnDFx/HljUjtzAt+mShdSyPZf50dnenrtcBWFEHHZvhWvqL+zAYRandx6z
7GC5oj6MsSb4uZt+PEsHVWLvycaOaOuKts2D5JKseexa6sC+40bdwI/CwOMTO2SswfwVqK6ZD/+fieTGZQ8sbIrkIQg9Rxv/vR88JK+86fg7L7A/w2LcaK3h
eoftfdGuoVq6PrNDOovSTVykDfVguNf3sfs+pKuFa/Mq4CBklpj6KqiCgU+w9M+WvaCRhZIT+ELejMByk0M/gi2J3Z1T32kKe8147NVj9tg986zuIvZrVqyC
CnpbCcE3E1kdauMs1gjfxYS1Q1aLNZf4z92owUYE/BagN8z+nQbls2AWhHPmWHTlWo7LoyUNP7PQ6sFrBeRdzAX0JL7jdujesf59kZU58Gzpod1YgtEQYO9i
no0LK8akb6kUMhxodcWHqQu0r5nPPPYiuGdLeBn6qzj6sHJoVLtoMxKwFg4LQR12HYpSvP1YtC8I3GikHJII6sqja2DllfiXaoiOBgZZ3sOPc8+ae7MH6/1o
8DE/A36k99SKI9ezOmFI1yNVJPI+cQyENApCQ+fI9T8z55zyxYSZBlfgFMB7fylLTHuPJM8ycFyHvCGGo8W6GPduh73DDPaeejFDWJjN6o6G/cvp3v7eKr7z
XJvMXJ96xPYo50TK2HvAIqbm5P8EjlXo3oPIEB7RKBvh+hHpdi76153b7qfuqH/bPf9w+f3tdac3/DCByb49hi2oo4vI2/sJ+icJ9xddoeRYucZk4vsAtgy7
zDShXVAKC8xZRIQQpMjxWYDgeQwF6Xu2vnN9p71/Vugcr5jgYXKyVsL1HyPmcyD1VeCxSih5sCbnWyUqOMjH9ywE8bwAJ6MWLj3wQftUAJXrkhQToFSqQIJC
CfTTnomTgqA1S1ZomJ2YJLOswOSs1ZozsC6A1rcZzpmNe1i4HiNt7cRn6wu6WoHYcuu6fwkydNvpTofjy1spT2AwfR7DYjyxAXUV+Lgz0s6tu1Ry8uYN8WPP
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
/nFdmK3Qvkgl21yFkhaSro9jBc/jx/sZLNhDxR4hR9FkwyGw7cqSQ3WRsBOrDQbQUJdNIKMtmKhVWshAdxi6JR1pcplMplSUcmYsmWVySpb5mi4MmJMASZyC
qmN9rtowHkOArGU6QQ9xUaYxFsGh9vMGLOv82zr0XktsRkYLFuwJw5EvKUF2DytcFBa3m2pF+JaYs1bcBsJLEg+j4rugC6lfcyrjJjewd2sVJpzKpP7C9eji
x8/w07b/t+Tt6tuZFK/QW92oinNMuKz9K33eNBc8dScOzM11LIS9MuzEs90I3UPPIT7tJr6BrD8x+Bx8AVBLAQIUAxQAAAAIAMZG71xC1Qo0txUAAOh8AABK
AAAAAAAAAAAAAACkgQAAAABzcmMvbWFpbi9qYXZhL25ldC9kcm9pbmdvL2FjdGlvbmNhbWVyYS9jbGllbnQvQWN0aW9uQ2FtZXJhQ2xpZW50U3RhdGUuamF2
YVBLAQIUAxQAAAAIANJG71y7YyFB6wEAABgHAABKAAAAAAAAAAAAAACkgR8WAABzcmMvbWFpbi9qYXZhL25ldC9kcm9pbmdvL2FjdGlvbmNhbWVyYS9jbGll
bnQvQWN0aW9uQ2FtZXJhS2V5TWFwcGluZ3MuamF2YVBLAQIUAxQAAAAIANJG71xSqgulQxIAAB5ZAABDAAAAAAAAAAAAAACkgXIYAABzcmMvbWFpbi9qYXZh
L25ldC9kcm9pbmdvL2FjdGlvbmNhbWVyYS9jbGllbnQvQ2xpZW50R2FtZUV2ZW50cy5qYXZhUEsBAhQDFAAAAAgA3UbvXH3AfqP/EgAASlIAAE8AAAAAAAAA
AAAAAKSBFisAAHNyYy9tYWluL2phdmEvbmV0L2Ryb2luZ28vYWN0aW9uY2FtZXJhL2NsaWVudC9ndWkvQWN0aW9uQ2FtZXJhVmlld2VyU2NyZWVuLmphdmFQ
SwECFAMUAAAACADqRu9cTsTMvFoBAABKBAAAPwAAAAAAAAAAAAAApIGCPgAAc3JjL21haW4vcmVzb3VyY2VzL2Fzc2V0cy9kcm9pbmdvX2FjdGlvbl9jYW1l
cmEvbGFuZy9lbl91cy5qc29uUEsFBgAAAAAFAAUASwIAADlAAAAAAA==
'@

try {
    $CleanBase64 = $PayloadBase64 -replace '\s', ''
    [System.IO.File]::WriteAllBytes(
        $PayloadZip,
        [System.Convert]::FromBase64String($CleanBase64)
    )

    Expand-Archive `
        -Path $PayloadZip `
        -DestinationPath $ExtractRoot `
        -Force

    $FilesToInstall = @(
        "src\main\java\net\droingo\actioncamera\client\ActionCameraClientState.java",
        "src\main\java\net\droingo\actioncamera\client\ActionCameraKeyMappings.java",
        "src\main\java\net\droingo\actioncamera\client\ClientGameEvents.java",
        "src\main\java\net\droingo\actioncamera\client\gui\ActionCameraViewerScreen.java",
        "src\main\resources\assets\droingo_action_camera\lang\en_us.json"
    )

    foreach ($RelativePath in $FilesToInstall) {
        $Source = Join-Path $ExtractRoot $RelativePath
        $Destination = Join-Path $ProjectRoot $RelativePath
        $Backup = Join-Path $BackupRoot $RelativePath

        if (!(Test-Path $Source)) {
            throw "Payload is missing: $RelativePath"
        }

        if (!(Test-Path $Destination)) {
            throw "Project is missing: $RelativePath"
        }

        New-Item -ItemType Directory -Force -Path (Split-Path $Backup -Parent) | Out-Null
        Copy-Item $Destination $Backup -Force

        New-Item -ItemType Directory -Force -Path (Split-Path $Destination -Parent) | Out-Null
        Copy-Item $Source $Destination -Force

        Write-Host "Installed: $RelativePath" -ForegroundColor Green
    }

    # Register the new configurable key mapping in the existing client mod event class.
    $ClientModEvents = Join-Path `
        $ProjectRoot `
        "src\main\java\net\droingo\actioncamera\client\ClientModEvents.java"

    if (!(Test-Path $ClientModEvents)) {
        throw "Could not find ClientModEvents.java, which is required to register the new keybind."
    }

    $ClientModEventsBackup = Join-Path `
        $BackupRoot `
        "src\main\java\net\droingo\actioncamera\client\ClientModEvents.java"

    New-Item `
        -ItemType Directory `
        -Force `
        -Path (Split-Path $ClientModEventsBackup -Parent) | Out-Null

    Copy-Item $ClientModEvents $ClientModEventsBackup -Force

    $ClientModContent = Get-Content $ClientModEvents -Raw

    if (
        $ClientModContent -notmatch
        'event\.register\(ActionCameraKeyMappings\.TOGGLE_OPERATOR_CONTROL\);'
    ) {
        $RegisterPattern =
            '(?m)^(\s*)event\.register\(ActionCameraKeyMappings\.[A-Z0-9_]+\);'

        $RegisterMatch = [regex]::Match(
            $ClientModContent,
            $RegisterPattern
        )

        if (!$RegisterMatch.Success) {
            throw "Could not find the existing key-mapping registration block in ClientModEvents.java."
        }

        $Indent = $RegisterMatch.Groups[1].Value
        $Insertion =
            $RegisterMatch.Value +
            [Environment]::NewLine +
            $Indent +
            'event.register(ActionCameraKeyMappings.TOGGLE_OPERATOR_CONTROL);'

        $ClientModContent =
            $ClientModContent.Substring(0, $RegisterMatch.Index) +
            $Insertion +
            $ClientModContent.Substring(
                $RegisterMatch.Index + $RegisterMatch.Length
            )

        $Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
        [System.IO.File]::WriteAllText(
            $ClientModEvents,
            $ClientModContent,
            $Utf8NoBom
        )

        Write-Host "Registered: Toggle Camera / Player Control" -ForegroundColor Green
    }
    else {
        Write-Host "Keybind already registered." -ForegroundColor Yellow
    }

    Write-Host ""
    Write-Host "Compiling..." -ForegroundColor Cyan

    & .\gradlew.bat compileJava

    if ($LASTEXITCODE -ne 0) {
        throw "Compilation failed."
    }

    Write-Host ""
    Write-Host "BUILD SUCCESSFUL - Operator Control installed." -ForegroundColor Green
    Write-Host ""
    Write-Host "Default key: Left Alt"
    Write-Host "The key can be changed in Minecraft Controls."
    Write-Host ""
    Write-Host "Viewer mode:"
    Write-Host "  - camera selector owns the mouse"
    Write-Host "  - groups, search and camera buttons remain available"
    Write-Host ""
    Write-Host "Player Control mode:"
    Write-Host "  - selected camera remains active"
    Write-Host "  - selector closes without leaving the camera"
    Write-Host "  - player, joystick and ReplayMod controls receive input"
    Write-Host "  - press the key again to reopen camera selection"
}
catch {
    Write-Host ""
    Write-Host "Patch failed. Restoring original files..." -ForegroundColor Red

    if (Test-Path $BackupRoot) {
        Get-ChildItem `
            -Path $BackupRoot `
            -Recurse `
            -File |
        ForEach-Object {
            $Relative = $_.FullName.Substring($BackupRoot.Length).TrimStart('\')
            $Destination = Join-Path $ProjectRoot $Relative

            New-Item `
                -ItemType Directory `
                -Force `
                -Path (Split-Path $Destination -Parent) | Out-Null

            Copy-Item $_.FullName $Destination -Force
        }
    }

    throw
}
finally {
    if (Test-Path $TempRoot) {
        Remove-Item $TempRoot -Recurse -Force
    }
}
