const localhost = '127.0.0.1:8017'

exports.name = 'KuiklyCore';
exports.rules = `
/.*/debug/nv_js/(.*)/ ${localhost}/$1
/.*/debug/nv_so/(.*)/ ${localhost}/$1
`;