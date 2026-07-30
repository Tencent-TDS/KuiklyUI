const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');

module.exports = (env, argv) => {
  const isDevelopment = argv.mode === 'development';

  return {
    entry: './src/index.js',
    output: {
      path: path.resolve(__dirname, 'build/distributions'),
      filename: 'h5App.js',
      clean: true,
    },
    devServer: {
      static: [
        {
          directory: path.join(__dirname, 'build/distributions'),
        },
        {
          directory: path.join(__dirname, 'src/resources'),
          publicPath: '/assets',
        }
      ],
      port: 8080,
      hot: true,
      open: true,
      historyApiFallback: true,
    },
    module: {
      rules: [
        {
          test: /\.js$/,
          exclude: /node_modules/,
          use: {
            loader: 'babel-loader',
            options: {
              presets: ['@babel/preset-env']
            }
          }
        }
      ]
    },
    plugins: [
      new HtmlWebpackPlugin({
        template: './src/resources/index.html',
        filename: 'index.html',
        inject: 'body',
        scriptLoading: 'blocking', // Ensure scripts are loaded synchronously
      }),
      new CopyWebpackPlugin({
        patterns: [
          {
            from: path.resolve(__dirname, '../demo/src/commonMain/assets'),
            to: path.resolve(__dirname, 'build/distributions/assets'),
            noErrorOnMissing: true,
          },
          // Copy Kotlin/JS libraries
          {
            from: path.resolve(__dirname, 'src/libs'),
            to: path.resolve(__dirname, 'build/distributions/libs'),
            noErrorOnMissing: false,
          },
          // Copy page bundles
          {
            from: path.resolve(__dirname, 'src/bundles'),
            to: path.resolve(__dirname, 'build/distributions/bundles'),
            noErrorOnMissing: true,
          },
          // Copy bundle configuration
          {
            from: path.resolve(__dirname, 'src/resources/bundle-config.js'),
            to: path.resolve(__dirname, 'build/distributions/bundle-config.js'),
            noErrorOnMissing: false,
          }
        ],
      }),
    ],
    resolve: {
      extensions: ['.js'],
      alias: {
        '@': path.resolve(__dirname, 'src'),
        '@components': path.resolve(__dirname, 'src/components'),
        '@modules': path.resolve(__dirname, 'src/modules'),
        '@utils': path.resolve(__dirname, 'src/utils'),
      }
    },
    devtool: isDevelopment ? 'source-map' : false,
  };
};
