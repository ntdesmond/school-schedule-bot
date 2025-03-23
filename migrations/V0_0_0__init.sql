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
    id           uuid primary key,
    name         text not null,
    time_slot_id uuid not null references timeslot (id)
);


create table classname
(
    id              uuid primary key,
    date            date not null,
    number          int  not null,
    letter          text not null,
    additional_data text
);

create table class_lesson
(
    class_name_id uuid not null references classname (id),
    lesson_id     uuid not null references lesson (id),
    primary key (class_name_id, lesson_id)
);
