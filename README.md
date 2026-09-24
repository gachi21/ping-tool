# Android Ping Tool

ابزار سبک و ترمینالی برای بررسی مداوم دسترسی شبکه در اندروید.

## Features
- Continuous reachability / ICMP-style ping
- Start / Stop
- Host یا IP با presetهای سریع
- Packet count (عدد 0 یعنی نامحدود)
- Interval و Timeout قابل تنظیم
- Packet loss
- Min / Average / Max latency
- Jitter
- خروجی زنده شبیه CMD
- Copy و Share گزارش
- Dark terminal UI
- GitHub Actions برای build و انتشار خودکار Release

## Build
`gradle assembleRelease`

APK در `app/build/outputs/apk/release/app-release.apk` ساخته می‌شود.

> توجه: اندروید در همه دستگاه‌ها اجازه ICMP خام را به یک شکل نمی‌دهد؛ این نسخه از InetAddress.isReachable() استفاده می‌کند، بنابراین رفتار ممکن است با ping دسکتاپ متفاوت باشد.
