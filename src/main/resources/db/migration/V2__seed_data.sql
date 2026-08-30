-- V2: seed data so a freshly provisioned cloud database has something to show.

INSERT INTO department (name) VALUES
    ('Engineering'),
    ('Finance'),
    ('Human Resources'),
    ('Sales');

INSERT INTO employee (first_name, last_name, email, hire_date, department_id) VALUES
    ('Ada',    'Lovelace', 'ada.lovelace@example.com',   '2019-03-11',
        (SELECT id FROM department WHERE name = 'Engineering')),
    ('Grace',  'Hopper',   'grace.hopper@example.com',   '2020-07-01',
        (SELECT id FROM department WHERE name = 'Engineering')),
    ('Alan',   'Turing',   'alan.turing@example.com',    '2021-01-18',
        (SELECT id FROM department WHERE name = 'Engineering')),
    ('Katherine', 'Johnson', 'k.johnson@example.com',    '2018-09-24',
        (SELECT id FROM department WHERE name = 'Finance')),
    ('Mary',   'Jackson',  'mary.jackson@example.com',   '2022-05-02',
        (SELECT id FROM department WHERE name = 'Human Resources')),
    ('Dorothy', 'Vaughan', 'dorothy.vaughan@example.com', '2017-11-13',
        (SELECT id FROM department WHERE name = 'Sales'));
