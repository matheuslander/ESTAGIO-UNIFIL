const {app, BrowserWindow, Menu} = require('electron');
const path = require('path');

function createWindow() {
    Menu.setApplicationMenu(null);

    const win = new BrowserWindow({
        width: 1360,
        height: 860,
        minWidth: 760,
        minHeight: 540,
        resizable: true,
        title: 'UniControl - Sistema de Gestão',
        icon: path.join(__dirname, '..', 'public', 'logo-uniao.png'),
        backgroundColor: '#05357a',
        autoHideMenuBar: true,
        titleBarStyle: 'hidden',
        titleBarOverlay: {
            color: '#05357a',
            symbolColor: '#ffffff',
            height: 42
        },
        webPreferences: {
            nodeIntegration: false,
            contextIsolation: true
        }
    });

    win.loadURL('http://localhost:5173').catch(() => {
        win.loadFile(path.join(__dirname, '..', 'dist', 'index.html'));
    });
}

app.whenReady().then(createWindow);

app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
});

app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') app.quit();
});
