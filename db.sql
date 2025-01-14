create table users (
	id integer primary key autoincrement,
	login text not null,
	password text not null,
	name text not null
);

create table roles (
	id integer primary key autoincrement,
	name text not null
);

create table users_to_roles (
	user_id integer not null,
	role_id integer not null,
	primary key (user_id, role_id),
	foreign key (user_id) references users(id) on delete cascade,
	foreign key (role_id) references roles(id) on delete cascade
);

insert into users (login, password, name) values ('qwe', 'qwe', 'qwe1');
insert into users (login, password, name) values ('asd', 'asd', 'asd1');
insert into users (login, password, name) values ('zxc', 'zxc', 'zxc1');
insert into users (login, password, name) values ('adm', 'adm', 'adm1');

insert into roles (name) values ('manager');
insert into roles (name) values ('admin');
insert into roles (name) values ('user');

insert into users_to_roles (user_id, role_id) values (1, 3);
insert into users_to_roles (user_id, role_id) values (2, 3);
insert into users_to_roles (user_id, role_id) values (3, 3);
insert into users_to_roles (user_id, role_id) values (4, 2);
insert into users_to_roles (user_id, role_id) values (3, 1);


