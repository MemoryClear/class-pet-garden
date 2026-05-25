const fs = require('fs');
const d = JSON.parse(fs.readFileSync('./evolution_rules.json', 'utf8'));

let stats = { pureLevel: 0, pureItem: 0, friendship: 0, trade: 0, mixed: 0, mega: 0, other: 0 };
const otherRules = [];

function isPureLv(c) {
  return /^等级[0-9]/.test(c) && c.indexOf('或') < 0 && c.indexOf('学会') < 0 && c.indexOf('究极') < 0 && c.indexOf('附近') < 0 && c.indexOf('后') < 0 && c.indexOf('对战中') < 0;
}
function isPureItem(c) {
  return (/^使用.+之石/.test(c) || /^使用.+之石（.+）$/.test(c)) && c.indexOf('或') < 0;
}
function isFriend(c) {
  return c.indexOf('亲密度') >= 0 || c.indexOf('友好度') >= 0 || (c.indexOf('学会') >= 0 && c.indexOf('提升等级') >= 0);
}
function isTrade(c) {
  return c.indexOf('连接交换') >= 0 || c.indexOf('联系绳') >= 0 || c.indexOf('王者之证') >= 0 || c.indexOf('升级数据') >= 0 || c.indexOf('可疑补丁') >= 0 || c.indexOf('金属膜') >= 0 || c.indexOf('护具') >= 0 || c.indexOf('龙之鳞片') >= 0;
}
function isMixed(c) {
  return (c.indexOf('或使用') >= 0 || c.indexOf('附近') >= 0) && c.indexOf('超级进化') < 0;
}
function isMega(c) {
  return c.indexOf('超级进化') >= 0 || c.indexOf('极巨化') >= 0;
}

Object.values(d).flat().forEach(r => {
  const c = r.condition || '';
  if (isPureLv(c)) stats.pureLevel++;
  else if (isPureItem(c)) stats.pureItem++;
  else if (isFriend(c)) stats.friendship++;
  else if (isTrade(c)) stats.trade++;
  else if (isMixed(c)) stats.mixed++;
  else if (isMega(c)) stats.mega++;
  else { stats.other++; otherRules.push({ toName: r.toName, condition: c }); }
});

console.log(JSON.stringify(stats, null, 2));
console.log('\nOther (need manual):');
otherRules.forEach(r => console.log('  ' + r.toName, '|', r.condition));
