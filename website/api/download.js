const { Readable } = require('stream');

module.exports = async function handler(req, res) {
  try {
    const upstreamUrl = 'https://github.com/hiharis/openview/releases/latest/download/openview.apk';
    const isHead = req.method === 'HEAD';

    const upstream = await fetch(upstreamUrl, {
      method: isHead ? 'HEAD' : 'GET',
      redirect: 'follow',
      headers: {
        'User-Agent': 'OpenView-Site'
      }
    });

    if (!upstream.ok || (!isHead && !upstream.body)) {
      res.setHeader('Content-Type', 'text/plain');
      res.setHeader('Cache-Control', 'no-store');
      return res.status(502).send('Download temporarily unavailable. Please try again in a minute.');
    }

    res.status(200);
    res.setHeader('Content-Type', 'application/vnd.android.package-archive');
    res.setHeader('Content-Disposition', 'attachment; filename="openview.apk"');
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('Cache-Control', 'no-store');

    const contentLength = upstream.headers.get('content-length');
    if (contentLength) {
      res.setHeader('Content-Length', contentLength);
    }

    if (isHead) {
      return res.end();
    }

    const readableStream = Readable.fromWeb(upstream.body);
    readableStream.on('error', () => {
      if (!res.headersSent) {
        res.status(502).end();
      } else {
        res.end();
      }
    });

    readableStream.pipe(res);
  } catch (err) {
    if (!res.headersSent) {
      res.setHeader('Content-Type', 'text/plain');
      res.setHeader('Cache-Control', 'no-store');
      res.status(502).send('Download temporarily unavailable. Please try again in a minute.');
    } else {
      res.end();
    }
  }
};
