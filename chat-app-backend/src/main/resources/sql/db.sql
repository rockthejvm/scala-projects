create table player(id text primary key, name text, email text, xp integer, game_type text);

-- Insert sample player data
INSERT INTO player (id, name, email, xp, game_type) VALUES
('p1', 'John Doe', 'john.doe@example.com', 1500, 'RPG'),
('p2', 'Jane Smith', 'jane.smith@example.com', 2200, 'FPS'),
('p3', 'Alex Johnson', 'alex.j@example.com', 980, 'Strategy'),
('p4', 'Maria Garcia', 'maria.g@example.com', 3100, 'MOBA'),
('p5', 'James Wilson', 'james.w@example.com', 1750, 'RPG'),
('p6', 'Sarah Lee', 'sarah.lee@example.com', 2850, 'FPS'),
('p7', 'Michael Brown', 'michael.b@example.com', 1320, 'Strategy'),
('p8', 'Emma Davis', 'emma.d@example.com', 2600, 'MOBA');

create table game(id text primary key, name text, game_type text);

-- Insert sample game data
INSERT INTO game (id, name, game_type) VALUES
('g1', 'The Elder Scrolls V: Skyrim', 'RPG'),
('g2', 'Final Fantasy XIV', 'RPG'),
('g3', 'Call of Duty: Modern Warfare', 'FPS'),
('g4', 'Counter-Strike 2', 'FPS'),
('g5', 'Civilization VI', 'Strategy'),
('g6', 'StarCraft II', 'Strategy'),
('g7', 'League of Legends', 'MOBA'),
('g8', 'Dota 2', 'MOBA'),
('g9', 'World of Warcraft', 'RPG'),
('g10', 'Apex Legends', 'FPS'),
('g11', 'Age of Empires IV', 'Strategy'),
('g12', 'Heroes of the Storm', 'MOBA');

