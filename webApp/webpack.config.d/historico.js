// Abrir /convite/CODIGO ou recarregar uma tela cai no index.html, como o Caddy faz em produção.
config.devServer = config.devServer || {};
config.devServer.historyApiFallback = true;
