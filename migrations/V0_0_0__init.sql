create table day_schedule
(
    date   date not null primary key,
    header text not null
);

create table timeslot
(
    id    uuid primary key,
    date  date not null,
    start time not null,
    "end" time not null
);

create table lesson
(
    id   uuid primary key,
    name text not null
);


create table classname
(
    id              uuid primary key,
    date            date not null,
    number          int  not null,
    letter          text not null,
    additional_data text
);

create table class_time_lesson
(
    class_name_id uuid not null references classname (id),
    timeslot_id   uuid not null references timeslot (id),
    lesson_id     uuid not null references lesson (id),
    primary key (class_name_id, timeslot_id, lesson_id)
);

create table subheader
(
    id          uuid primary key,
    date        date not null,
    timeslot_id uuid references timeslot (id),
    content     text not null
);

