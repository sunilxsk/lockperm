package io.github.sunilxsk.lockperm

internal val DEFAULT_JS = """
(function() {
    const existingBanner = document.getElementById('colorful-banner-123456789');
    if (existingBanner) existingBanner.remove();

    const banner = document.createElement('div');
    banner.id = 'colorful-banner-123456789';
    banner.textContent = '你好你好你好，这是 LockPerm 的模块 ';
    if (!document.getElementById('colorful-banner-style-123456789')) {
        const style = document.createElement('style');
        style.id = 'colorful-banner-style-123456789';
        style.textContent = `
            #colorful-banner-123456789 {
                position: fixed; top: 12px; left: 16px;
                padding: 6px 16px; border-radius: 20px;
                font-size: 18px; font-weight: bold; letter-spacing: 4px;
                z-index: 2147483647; background: transparent;
                background-image: linear-gradient(90deg, #87cefa, #00bfff, #d8b4fe, #90ee90, #ffb6c1, #87cefa);
                background-size: 300% 100%;
                -webkit-background-clip: text; background-clip: text;
                -webkit-text-fill-color: transparent; color: transparent;
                animation: banner-text-flow 4s linear infinite;
                filter: drop-shadow(0 0 6px rgba(135,206,250,0.9))
                        drop-shadow(0 0 12px rgba(216,180,254,0.6));
                pointer-events: none; user-select: none;
            }
            @keyframes banner-text-flow {
                0% { background-position: 0% 50%; }
                100% { background-position: 300% 50%; }
            }
        `;
        document.head.appendChild(style);
    }
    document.body.insertBefore(banner, document.body.firstChild);
})();
""".trimIndent()

internal object XpDefaults {
    const val ANDROID_ID = ""

    
    val JS: String get() = DEFAULT_JS
}
