INSERT INTO app_user (id, display_name, email, avatar_color) VALUES
('demo-lin', '林小满', 'xiaoman@example.test', '#B6F34A'),
('demo-chen', '陈屿', 'chenyu@example.test', '#79C8FF');

INSERT INTO template
    (id, title, category, cover_url, description, required_image_count, accepted_mime_types, max_file_size_bytes)
VALUES
('travel-postcard', '旅行明信片', '创意图片', '/api/media/templates/travel.svg',
 '把旅途里的两张照片排成一张有呼吸感的明信片，适合风景、街道或同行瞬间。', 2, 'image/jpeg,image/png', 5242880),
('coffee-poster', '咖啡时刻', '品牌海报', '/api/media/templates/coffee.svg',
 '用一张主图生成温暖的咖啡新品海报，自动加入留白、标题与细节点缀。', 1, 'image/jpeg,image/png', 5242880),
('pet-portrait', '毛孩子写真', '生活记录', '/api/media/templates/pet.svg',
 '为宠物照片制作一张杂志感肖像封面，保留主体并叠加柔和色彩。', 1, 'image/jpeg,image/png', 8388608),
('wedding-story', '双人故事', '人物合影', '/api/media/templates/wedding.svg',
 '将两张人物照片组合成纪念卡片，适合双人合照与重要时刻。', 2, 'image/jpeg,image/png', 10485760),
('new-year-card', '新岁贺卡', '节日营销', '/api/media/templates/new-year.svg',
 '用两张素材制作明亮的新年祝福卡片，生成稳定的方形社交分享图。', 2, 'image/jpeg,image/png', 5242880),
('memory-collage', '记忆拼贴', '照片合集', '/api/media/templates/collage.svg',
 '把三张照片整理成一张有节奏的拼贴画，适合旅行、聚会和日常记录。', 3, 'image/jpeg,image/png', 8388608);
