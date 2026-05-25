const fs = require('fs');
const d = JSON.parse(fs.readFileSync('./data/evolution_rules.json', 'utf8'));

let stats = { pureLevel: 0, pureItem: 0, friendship: 0, trade: 0, mixed: 0, mega: 0, other: 0 };
const otherRules = [];

Object.values(d).flat().forEach(r => {
  const c = r.condition || '';
  const isPureLv = /^等级[0-9]/.test(c) && !c.includes('或') && !c.includes('学会') && !c.includes('究极') && !c.includes('附近') && !c.includes('后') && !c.includes('对战中');
  const isPureItem = (/^使用.+之石/.test(c) || /^使用.+之石（.+）$/.test(c)) && !c.includes('或');
  const isFriend = c.includes('亲密度') || c.includes('友好度') || (c.includes('学会') && c.includes('提升等级'));
  const isTrade = c.includes('连接交换') || c.includes('联系绳') || c.includes('王者之证') || c.includes('升级数据') || c.includes('可疑补丁') || c.includes('金属膜') || c.includes('护具') || c.includes('龙之鳞片');
  const isMixed = (c.includes('或使用') || c.includes('附近')) && !c.includes('超级进化');
  const isMega = c.includes('超级进化') || c.includes('极巨化');

  if (isPureLv) stats.pureLevel++;
  else if (isPureItem) stats.pureItem++;
  else if (isFriend) stats.friendship++;
  else if (isTrade) stats.trade++;
  else if (isMixed) stats.mixed++;
  else if (isMega) stats.mega++;
  else {
    stats.other++;
    otherRules.push({ toName: r.toName, condition: c });
  }
});

console.log(JSON.stringify(stats, null, 2));
console.log('\nOther rules:');
otherRules.forEach(r => console.log(r.toName, '|', r.condition));
